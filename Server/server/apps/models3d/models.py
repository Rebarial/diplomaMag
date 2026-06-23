from django.db import models
from django.contrib.auth.models import User
from apps.drawings.models import Drawing


class Model3D(models.Model):
    GENERATION_MODES = [
        ('simple', 'Simple'),
        ('advanced', 'Advanced'),
        ('upload', 'Upload'),
    ]

    user = models.ForeignKey(User, on_delete=models.CASCADE)
    name = models.CharField(max_length=50)
    file = models.FileField(upload_to='models/%Y/%m/%d/')
    drawing = models.ForeignKey(Drawing, null=True, blank=True, on_delete=models.SET_NULL)
    generation_mode = models.CharField(max_length=20, choices=GENERATION_MODES)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ('user', 'name')

    def __str__(self):
        return f"{self.user.username} / {self.name}"