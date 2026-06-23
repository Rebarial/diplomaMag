import re
from rest_framework import serializers
from .models import Model3D


class ModelListSerializer(serializers.ModelSerializer):
    class Meta:
        model = Model3D
        fields = ['id', 'name', 'generation_mode', 'created_at']


class ModelDetailSerializer(serializers.ModelSerializer):
    class Meta:
        model = Model3D
        fields = ['id', 'name', 'file_url', 'generation_mode', 'created_at']


class ModelUploadSerializer(serializers.Serializer):
    name = serializers.CharField()
    file = serializers.FileField()

    def validate_name(self, value):
        if not re.match(r'^[A-Za-z0-9_ -]+$', value):
            raise serializers.ValidationError("Имя модели содержит недопустимые символы (E.08)")
        if not 1 <= len(value) <= 50:
            raise serializers.ValidationError("Длина имени модели вне диапазона 1–50 (E.09)")
        return value

    def validate_file(self, value):
        if not value.name.endswith('.glb'):
            raise serializers.ValidationError("Неподдерживаемый формат модели (E.22)")
        content = value.read(4)
        value.seek(0)
        if content != b'glTF':
            raise serializers.ValidationError("Файл модели повреждён (E.23)")
        return value