from django.urls import path
from .views import ModelListView, ModelDetailView, ModelUploadView

urlpatterns = [
    path('', ModelListView.as_view()),
    path('<int:pk>/', ModelDetailView.as_view()),
    path('upload/', ModelUploadView.as_view()),
]