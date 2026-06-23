from rest_framework.views import APIView
from rest_framework.response import Response
from .models import Drawing
from .serializers import DrawingListSerializer
from apps.errors import err as e


class DrawingListView(APIView):

    def get(self, request):
        drawings = Drawing.objects.filter(user=request.user).order_by('-uploaded_at')
        data = [
            {
                "id": d.id,
                "name": d.name,
                "uploaded_at": d.uploaded_at.isoformat(),
                "file": request.build_absolute_uri(d.file.url),
            }
            for d in drawings
        ]
        return Response(data, status=200)


class DrawingDetailView(APIView):

    def delete(self, request, pk):
        try:
            drawing = Drawing.objects.get(pk=pk)
        except Drawing.DoesNotExist:
            return Response(e("E.28"), status=404)

        if drawing.user != request.user:
            return Response(e("E.28"), status=403)

        drawing.file.delete(save=False)
        drawing.delete()
        return Response({"status": "ok"}, status=200)