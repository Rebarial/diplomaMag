import re
import base64
import json
import logging
from ollama import Client
from tools.llm.llm_config_loader import LLMConfig

logger = logging.getLogger(__name__)

_PARAM_ORDER = {
    "cuboid":           ["length", "width", "height"],
    "cylinder":         ["radius", "height"],
    "cone":             ["radius", "height"],
    "sphere":           ["radius"],
    "pyramid":          ["length", "width", "height"],
    "prism":            ["radius", "height"],
    "triangular_prism": ["radius", "height"],
    "torus":            ["major_radius", "minor_radius"],
}


class LLMManager:

    def __init__(self):
        simple_cfg = LLMConfig.get_model("simple")
        advanced_cfg = LLMConfig.get_model("advanced")
        offset_cfg = LLMConfig.get_model("offset")
        ocr_cfg = LLMConfig.get_model("ocr")

        self.__simple_model = simple_cfg["name"]
        self.__simple_prompt = simple_cfg["prompt_template"]

        self.__advanced_model = advanced_cfg["name"]
        self.__advanced_prompt = advanced_cfg["prompt_template"]

        self.__offset_model = offset_cfg["name"]
        self.__offset_prompt = offset_cfg["prompt_template"]

        self.__ocr_model = ocr_cfg["name"]
        self.__ocr_prompt = ocr_cfg["prompt_template"]

        self.__client = Client(host=LLMConfig.get_ollama_url())

    # ── shared ──────────────────────────────────────────────────────────────

    def __encode_image(self, image_path: str) -> str:
        with open(image_path, "rb") as f:
            return base64.b64encode(f.read()).decode('utf-8')

    def __extract_numbers(self, image_b64: str) -> list:
        try:
            response = self.__client.chat(
                model=self.__ocr_model,
                messages=[{
                    'role': 'user',
                    'content': self.__ocr_prompt,
                    'images': [image_b64],
                }]
            )
            text = response['message']['content']
            logger.debug("[LLM ocr] raw response: %s", text)
            numbers = []
            for token in re.findall(r'\d+(?:[.,]\d+)?', text):
                val = float(token.replace(',', '.'))
                if val > 0:
                    numbers.append(val)
            numbers.sort()
            logger.debug("[LLM ocr] extracted: %s", numbers)
            return numbers
        except Exception as exc:
            logger.warning("[LLM ocr] failed: %s", exc)
            return []

    def __normalize(self, figures: list) -> list:
        result = []
        for fig in figures:
            if "dimensions" not in fig:
                keys = _PARAM_ORDER.get(fig.get("type", ""), [])
                dims = [fig[k] for k in keys if k in fig]
                if dims:
                    fig = {k: v for k, v in fig.items() if k not in keys}
                    fig["dimensions"] = dims
            dims = fig.get("dimensions", [])
            if not dims:
                raise ValueError(f"Figure '{fig.get('type')}' has no dimensions")
            if any(d == 0 for d in dims):
                raise ValueError(f"Figure '{fig.get('type')}' has unreadable dimension: {dims}")
            if not fig.get("offset"):
                fig["offset"] = [0, 0, 0]
            if not fig.get("rotation"):
                fig["rotation"] = [0, 0, 0]
            result.append(fig)
        return result

    def __nums_str(self, numbers: list) -> str:
        return ', '.join(str(int(n) if n == int(n) else n) for n in numbers)

    # ── simple ───────────────────────────────────────────────────────────────

    def __parse_object(self, text: str) -> dict:
        text = re.sub(r'```json|```', '', text)
        start = text.find('{')
        if start == -1:
            raise json.JSONDecodeError("No '{' found", text, 0)
        end = text.rfind('}')
        if end == -1:
            raise json.JSONDecodeError("No '}' found", text, 0)
        return json.loads(text[start:end + 1])

    def classify_simple(self, image_path: str) -> dict:
        image_b64 = self.__encode_image(image_path)

        numbers = self.__extract_numbers(image_b64)
        user_content = 'Analyze this engineering drawing and return ONLY the JSON object.'
        if numbers:
            user_content += f'\nNumbers found in the drawing: {self.__nums_str(numbers)}'
            logger.debug("[LLM simple] injected numbers: %s", numbers)

        response = self.__client.chat(
            model=self.__simple_model,
            messages=[
                {'role': 'system', 'content': self.__simple_prompt},
                {'role': 'user', 'content': user_content, 'images': [image_b64]},
            ]
        )
        raw = response['message']['content']
        logger.debug("[LLM simple] raw response: %s", raw)

        try:
            parsed = self.__parse_object(raw)
        except json.JSONDecodeError as exc:
            logger.warning("[LLM simple] JSON parse error: %s | raw: %s", exc, raw)
            raise ValueError(f"LLM returned unexpected format: {raw}")

        figures = self.__normalize([parsed])
        if numbers:
            unknown = self.__unknown_dimensions(figures, numbers)
            if unknown:
                raise ValueError(f"[LLM simple] dimensions not found in OCR: {unknown}")
        logger.debug("[LLM simple] parsed figures: %s", figures)
        return {"figures": figures}

    # ── advanced ─────────────────────────────────────────────────────────────

    def __parse_array(self, text: str) -> list:
        text = re.sub(r'```json|```', '', text)
        start = text.find('[')
        if start == -1:
            raise json.JSONDecodeError("No '[' found", text, 0)
        text = text[start:].strip()
        text = re.sub(r'Math\.PI\s*/\s*2', '90', text)
        text = re.sub(r'Math\.PI\s*\*\s*2', '360', text)
        text = re.sub(r'Math\.PI', '180', text)
        text = re.sub(r'//[^\n]*', '', text)
        parsed = json.loads(text)
        if isinstance(parsed, list) and len(parsed) == 1 and isinstance(parsed[0], list):
            parsed = parsed[0]
        return parsed

    def __missing_numbers(self, figures: list, numbers: list) -> list:
        used = set()
        for fig in figures:
            for val in fig.get('dimensions', []):
                used.add(float(val))
        return [n for n in numbers if float(n) not in used]

    def __unknown_dimensions(self, figures: list, numbers: list) -> list:
        ocr_set = {float(n) for n in numbers}
        unknown = []
        for fig in figures:
            for val in fig.get('dimensions', []):
                if float(val) not in ocr_set:
                    unknown.append((fig.get('type'), val))
        return unknown

    def classify_advanced(self, image_path: str) -> dict:
        image_b64 = self.__encode_image(image_path)

        numbers = self.__extract_numbers(image_b64)
        nums_str = self.__nums_str(numbers) if numbers else ''
        user_content = 'Analyze this engineering drawing and return ONLY the JSON array.'
        if numbers:
            user_content += f'\nNumbers found in the drawing: {nums_str}'
            logger.debug("[LLM advanced] injected numbers: %s", nums_str)

        messages = [
            {'role': 'system', 'content': self.__advanced_prompt},
            {'role': 'user', 'content': user_content, 'images': [image_b64]},
        ]

        response = self.__client.chat(model=self.__advanced_model, messages=messages)
        raw = response['message']['content']
        logger.debug("[LLM advanced] raw response: %s", raw)

        try:
            parsed = self.__parse_array(raw)
        except json.JSONDecodeError as exc:
            logger.warning("[LLM advanced] JSON parse error: %s | raw: %s", exc, raw)
            raise

        if numbers:
            missing = self.__missing_numbers(parsed, numbers)
            if missing:
                missing_str = self.__nums_str(missing)
                logger.debug("[LLM advanced] missing numbers: %s — retrying", missing_str)

                messages.append({'role': 'assistant', 'content': raw})
                messages.append({
                    'role': 'user',
                    'content': (
                        f'Wrong. All numbers from the drawing: {nums_str}. '
                        f'You are missing: {missing_str}. '
                        f'Every number must appear in dimensions. '
                        f'Return the corrected JSON array only.'
                    ),
                    'images': [image_b64],
                })

                retry_response = self.__client.chat(model=self.__advanced_model, messages=messages)
                retry_raw = retry_response['message']['content']
                logger.debug("[LLM advanced] retry response: %s", retry_raw)

                try:
                    parsed = self.__parse_array(retry_raw)
                except json.JSONDecodeError:
                    logger.warning("[LLM advanced] retry parse failed, using first response")

        figures = self.__normalize(parsed)
        if numbers:
            unknown = self.__unknown_dimensions(figures, numbers)
            if unknown:
                raise ValueError(f"[LLM advanced] dimensions not found in OCR: {unknown}")

        if len(figures) > 1:
            figures = self.__resolve_offsets(image_b64, figures)

        figures.sort(key=lambda f: f.get("offset", [0, 0, 0])[2])
        logger.debug("[LLM advanced] parsed figures: %s", figures)
        return {"figures": figures}

    def __resolve_offsets(self, image_b64: str, figures: list) -> list:
        fig_list = json.dumps([
            {"type": f["type"], "dimensions": f["dimensions"]} for f in figures
        ])
        response = self.__client.chat(
            model=self.__offset_model,
            messages=[
                {'role': 'system', 'content': self.__offset_prompt},
                {'role': 'user', 'content': f'Figures: {fig_list}', 'images': [image_b64]},
            ]
        )
        raw = response['message']['content']
        logger.debug("[LLM offset] raw: %s", raw)

        raw_clean = re.sub(r'```json|```', '', raw)
        start = raw_clean.find('[')
        end = raw_clean.rfind(']')
        result = json.loads(raw_clean[start:end + 1])

        if len(result) != len(figures):
            raise ValueError(f"[LLM offset] expected {len(figures)} figures, got {len(result)}")
        for fig in result:
            if 'offset' not in fig:
                raise ValueError(f"[LLM offset] missing offset in: {fig}")
            if 'rotation' not in fig:
                fig['rotation'] = [0, 0, 0]
        return result
