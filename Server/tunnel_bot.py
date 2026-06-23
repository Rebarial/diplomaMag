"""
Telegram-бот для управления SSH-туннелем localhost.run.

Команды:
  /tunnel  — запустить туннель, получить URL (автоотключение через 15 минут)
  /stop    — остановить туннель вручную
  /status  — статус туннеля
  /start   — справка
"""

import asyncio
import logging
import os
import re

from telegram import Update
from telegram.ext import Application, CommandHandler, ContextTypes

BOT_TOKEN: str = "8816398339:AAHLWitAEO49a752C-hPR9B67xNsSKYyYuA"
LOCAL_PORT: int = int(os.getenv("TUNNEL_LOCAL_PORT", "8000"))
TUNNEL_TIMEOUT: int = 15 * 60  # секунды

logging.basicConfig(
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    level=logging.INFO,
)
logger = logging.getLogger(__name__)

_URL_RE = re.compile(r"https://[a-zA-Z0-9]+\.lhr\.life")

tunnel_proc: asyncio.subprocess.Process | None = None
tunnel_url: str | None = None
stop_task: asyncio.Task | None = None


async def _read_until_url(proc: asyncio.subprocess.Process) -> str | None:
    """Читает stdout процесса строка за строкой, возвращает первый найденный URL."""
    while True:
        line = await proc.stdout.readline()
        if not line:
            return None
        decoded = line.decode("utf-8", errors="replace").strip()
        logger.info("tunnel >> %s", decoded)
        match = _URL_RE.search(decoded)
        if match:
            return match.group(0)


async def _wait_for_url(proc: asyncio.subprocess.Process, timeout: float = 60.0) -> str | None:
    try:
        return await asyncio.wait_for(_read_until_url(proc), timeout=timeout)
    except asyncio.TimeoutError:
        return None


async def _kill_tunnel() -> None:
    global tunnel_proc, tunnel_url
    if tunnel_proc is not None:
        try:
            tunnel_proc.kill()
            await tunnel_proc.wait()
        except Exception:
            pass
        tunnel_proc = None
    tunnel_url = None


async def _auto_stop(bot, chat_id: int) -> None:
    global stop_task
    await asyncio.sleep(TUNNEL_TIMEOUT)
    await _kill_tunnel()
    stop_task = None
    try:
        await bot.send_message(
            chat_id=chat_id,
            text="⏰ Туннель автоматически остановлен — прошло 15 минут.",
        )
    except Exception as exc:
        logger.warning("Не удалось отправить сообщение об автоотключении: %s", exc)


async def cmd_start(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    await update.message.reply_text(
        "Команды:\n"
        "/tunnel — запустить туннель (автоотключение через 15 мин)\n"
        "/stop   — остановить туннель вручную\n"
        "/status — статус туннеля"
    )


async def cmd_tunnel(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    global tunnel_proc, tunnel_url, stop_task

    if tunnel_proc is not None and tunnel_proc.returncode is None:
        await update.message.reply_text(f"Туннель уже запущен:\n{tunnel_url}")
        return

    await update.message.reply_text("Запускаю туннель...")

    try:
        tunnel_proc = await asyncio.create_subprocess_exec(
            "ssh",
            "-o", "StrictHostKeyChecking=no",
            "-o", "ServerAliveInterval=30",
            "-R", f"80:localhost:{LOCAL_PORT}",
            "nokey@localhost.run",
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.STDOUT,
        )
    except FileNotFoundError:
        await update.message.reply_text("Ошибка: команда ssh не найдена. Установите OpenSSH.")
        tunnel_proc = None
        return
    except Exception as exc:
        await update.message.reply_text(f"Ошибка запуска SSH: {exc}")
        tunnel_proc = None
        return

    url = await _wait_for_url(tunnel_proc, timeout=60)

    if url is None:
        await update.message.reply_text(
            "Не удалось получить URL туннеля за 60 секунд.\n"
            "Проверьте, запущен ли Django на порту 8000 и доступен ли SSH."
        )
        await _kill_tunnel()
        return

    tunnel_url = url

    await update.message.reply_text(
        f"Туннель запущен!\n\n"
        f"URL: {url}\n\n"
        f"Автоматически остановится через 15 минут.\n"
        f"Для ручной остановки: /stop"
    )

    if stop_task is not None:
        stop_task.cancel()
    chat_id = update.effective_chat.id
    stop_task = asyncio.create_task(_auto_stop(context.bot, chat_id))


async def cmd_stop(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    global stop_task

    if tunnel_proc is None or tunnel_proc.returncode is not None:
        await update.message.reply_text("Туннель не запущен.")
        return

    if stop_task is not None:
        stop_task.cancel()
        stop_task = None

    await _kill_tunnel()
    await update.message.reply_text("Туннель остановлен.")


async def cmd_status(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    if tunnel_proc is None or tunnel_proc.returncode is not None:
        await update.message.reply_text("Туннель не запущен.")
    else:
        await update.message.reply_text(f"Туннель работает:\n{tunnel_url}")


def main() -> None:
    if BOT_TOKEN == "YOUR_BOT_TOKEN_HERE":
        raise SystemExit(
            "Укажите токен бота: задайте переменную окружения TUNNEL_BOT_TOKEN\n"
            "или замените 'YOUR_BOT_TOKEN_HERE' в файле tunnel_bot.py"
        )

    app = Application.builder().token(BOT_TOKEN).build()
    app.add_handler(CommandHandler("start", cmd_start))
    app.add_handler(CommandHandler("tunnel", cmd_tunnel))
    app.add_handler(CommandHandler("stop", cmd_stop))
    app.add_handler(CommandHandler("status", cmd_status))

    logger.info("Бот запущен. Ожидание команд...")
    app.run_polling(allowed_updates=Update.ALL_TYPES)


if __name__ == "__main__":
    main()
