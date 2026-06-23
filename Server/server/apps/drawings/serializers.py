import re
from PIL import Image
from rest_framework import serializers
from .models import Drawing


class DrawingListSerializer(serializers.ModelSerializer):
    class Meta:
        model = Drawing
        fields = ['id', 'name', 'uploaded_at']


def _validate_image_file(value):
    if not value.name.lower().endswith(('.png', '.jpg', '.jpeg')):
        raise serializers.ValidationError("Неподдерживаемый формат файла (E.16)")
    if value.size > 10 * 1024 * 1024:
        raise serializers.ValidationError("Размер файла превышает 10 МБ (E.17)")
    try:
        img = Image.open(value)
        img.verify()
        value.seek(0)
        img = Image.open(value)
        if img.width < 400 or img.height < 400:
            raise serializers.ValidationError("Разрешение изображения ниже 400×400 (E.18)")
    except serializers.ValidationError:
        raise
    except Exception:
        raise serializers.ValidationError("Файл чертежа повреждён (E.19)")
    value.seek(0)
    return value


class DrawingUploadSerializer(serializers.Serializer):
    drawing_name = serializers.CharField()
    model_name = serializers.CharField()
    file = serializers.ImageField()

    def validate_drawing_name(self, value):
        if not re.match(r'^[A-Za-z0-9_-]+$', value):
            raise serializers.ValidationError("Имя чертежа содержит недопустимые символы (E.10)")
        if not 1 <= len(value) <= 50:
            raise serializers.ValidationError("Длина имени чертежа вне диапазона 1–50 (E.11)")
        return value

    def validate_model_name(self, value):
        if not re.match(r'^[A-Za-z0-9_ -]+$', value):
            raise serializers.ValidationError("Имя модели содержит недопустимые символы (E.08)")
        if not 1 <= len(value) <= 50:
            raise serializers.ValidationError("Длина имени модели вне диапазона 1–50 (E.09)")
        return value

    def validate_file(self, value):
        return _validate_image_file(value)


class DrawingAdvancedUploadSerializer(serializers.Serializer):
    file = serializers.ImageField()

    def validate_file(self, value):
        return _validate_image_file(value)
