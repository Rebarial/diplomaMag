from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.parsers import MultiPartParser
from .models import Model3D
from .serializers import ModelUploadSerializer
from apps.errors import err as e

class ModelListView(APIView):

    def get(self, request):
        models = Model3D.objects.filter(user=request.user).order_by('-created_at')
        data = [
            {
                "id": m.id,
                "name": m.name,
                "generation_mode": m.generation_mode,
                "created_at": m.created_at.isoformat(),
            }
            for m in models
        ]
        return Response(data, status=200)


class ModelDetailView(APIView):

    def get(self, request, pk):
        try:
            model = Model3D.objects.get(pk=pk)
        except Model3D.DoesNotExist:
            return Response(e("E.26"), status=404)

        if model.user != request.user:
            return Response(e("E.27"), status=403)

        return Response({
            "id": model.id,
            "name": model.name,
            "file_url": model.file.url,
            "generation_mode": model.generation_mode,
            "created_at": model.created_at.isoformat(),
        }, status=200)

    def delete(self, request, pk):
        try:
            model = Model3D.objects.get(pk=pk)
        except Model3D.DoesNotExist:
            return Response(e("E.26"), status=404)

        if model.user != request.user:
            return Response(e("E.27"), status=403)

        model.file.delete(save=False)
        model.delete()
        return Response({"status": "ok"}, status=200)


class ModelUploadView(APIView):
    parser_classes = [MultiPartParser]

    def post(self, request):
        serializer = ModelUploadSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))[0]
            return Response({"error": str(first_error)}, status=400)

        name = serializer.validated_data['name']
        file = serializer.validated_data['file']

        if Model3D.objects.filter(user=request.user, name=name).exists():
            return Response(e("E.24"), status=409)

        model = Model3D.objects.create(
            user=request.user,
            name=name,
            file=file,
            generation_mode='advanced',
        )
        return Response({
            "id": model.id,
            "name": model.name,
            "file_url": request.build_absolute_uri(model.file.url),
            "generation_mode": model.generation_mode,
            "created_at": model.created_at.isoformat(),
        }, status=200)