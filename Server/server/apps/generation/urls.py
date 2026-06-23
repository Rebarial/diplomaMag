from django.urls import path
from .views import SimpleGenerateView, AdvancedGenerateView

urlpatterns = [
    path('simple/', SimpleGenerateView.as_view()),
    path('advanced/', AdvancedGenerateView.as_view()),
]