from django.urls import path, include

urlpatterns = [
    path('auth/', include('apps.users.urls')),
    path('models/', include('apps.models3d.urls')),
    path('drawings/', include('apps.drawings.urls')),
    path('generate/', include('apps.generation.urls')),
]