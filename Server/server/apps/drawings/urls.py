from django.urls import path
from .views import DrawingListView, DrawingDetailView

urlpatterns = [
    path('', DrawingListView.as_view()),
    path('<int:pk>/', DrawingDetailView.as_view()),
]