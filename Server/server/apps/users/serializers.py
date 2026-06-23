import re
from rest_framework import serializers


class RegisterSerializer(serializers.Serializer):
    username = serializers.CharField()
    password = serializers.CharField()

    def validate_username(self, value):
        if not re.match(r'^[A-Za-z0-9_-]+$', value):
            raise serializers.ValidationError("Логин содержит недопустимые символы (E.04)")
        if not 5 <= len(value) <= 20:
            raise serializers.ValidationError("Длина логина вне диапазона 5–20 (E.05)")
        return value

    def validate_password(self, value):
        if not re.match(r'^[A-Za-z0-9!@#$%^&*()_+\-=]+$', value):
            raise serializers.ValidationError("Пароль содержит недопустимые символы (E.06)")
        if not 8 <= len(value) <= 40:
            raise serializers.ValidationError("Длина пароля вне диапазона 8–40 (E.07)")
        return value


class LoginSerializer(serializers.Serializer):
    username = serializers.CharField()
    password = serializers.CharField()