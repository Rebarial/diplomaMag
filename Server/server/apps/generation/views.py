from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.parsers import MultiPartParser
from django.core.files.base import ContentFile

from apps.errors import err as e
from apps.drawings.models import Drawing
from apps.drawings.serializers import DrawingUploadSerializer, DrawingAdvancedUploadSerializer
from apps.models3d.models import Model3D
from tools.llm.llm_manager import LLMManager
from tools.figures import figures_dict
from tools.converter import obj_content_to_glb, FIGURE_COLORS
import tempfile
import os
import httpx


def _resolve_figure_class(fig_type: str):
    """Exact match first, then partial substring match."""
    if fig_type in figures_dict:
        return figures_dict[fig_type]
    for key, cls in figures_dict.items():
        if key in fig_type:
            return cls
    return None


class SimpleGenerateView(APIView):
    parser_classes = [MultiPartParser]

    def post(self, request):
        serializer = DrawingUploadSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))[0]
            return Response({"error": str(first_error)}, status=400)

        drawing_name = serializer.validated_data['drawing_name']
        model_name = serializer.validated_data['model_name']
        file = serializer.validated_data['file']

        if Drawing.objects.filter(user=request.user, name=drawing_name).exists():
            return Response(e("E.21"), status=409)

        if Model3D.objects.filter(user=request.user, name=model_name).exists():
            return Response(e("E.24"), status=409)

        drawing = Drawing.objects.create(
            user=request.user,
            name=drawing_name,
            file=file,
        )

        try:
            llm = LLMManager()
            result = llm.classify_simple(drawing.file.path)
        except (httpx.ConnectError, httpx.TimeoutException):
            drawing.file.delete(save=False)
            drawing.delete()
            return Response(e("E.29"), status=503)
        except Exception:
            drawing.file.delete(save=False)
            drawing.delete()
            return Response(e("E.20"), status=400)

        fig = result["figures"][0]
        figure_class_fn = _resolve_figure_class(fig["type"].lower())

        if figure_class_fn is None:
            drawing.file.delete(save=False)
            drawing.delete()
            return Response(e("E.20"), status=400)

        try:
            figure = figure_class_fn(fig["dimensions"])
        except Exception:
            drawing.file.delete(save=False)
            drawing.delete()
            return Response(e("E.20"), status=400)

        try:
            obj_content = figure.to_obj_string()
            tmp_path = os.path.join(tempfile.gettempdir(), f"{model_name}.obj")
            glb_bytes = obj_content_to_glb(obj_content, tmp_path, color=FIGURE_COLORS[0])
        except Exception:
            drawing.file.delete(save=False)
            drawing.delete()
            return Response(e("E.30"), status=500)

        glb_filename = f"{model_name}.glb"
        model = Model3D.objects.create(
            user=request.user,
            name=model_name,
            file=ContentFile(glb_bytes, name=glb_filename),
            drawing=drawing,
            generation_mode='simple',
        )

        return Response({
            "model_id": model.id,
            "model_name": model.name,
            "file_url": model.file.url,
            "detected_class": fig["type"],
            "detected_dimensions": fig["dimensions"],
            "generation_mode": "simple",
        }, status=200)


class AdvancedGenerateView(APIView):
    parser_classes = [MultiPartParser]

    def post(self, request):
        serializer = DrawingAdvancedUploadSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))[0]
            return Response({"error": str(first_error)}, status=400)

        file = serializer.validated_data['file']

        tmp_path = None
        try:
            suffix = os.path.splitext(file.name)[1]
            with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
                for chunk in file.chunks():
                    tmp.write(chunk)
                tmp_path = tmp.name

            manager = LLMManager()
            result = manager.classify_advanced(tmp_path)
        except (httpx.ConnectError, httpx.TimeoutException):
            return Response(e("E.29"), status=503)
        except Exception:
            return Response(e("E.20"), status=400)
        finally:
            if tmp_path:
                try:
                    os.remove(tmp_path)
                except Exception:
                    pass

        primitives = []

        for idx, fig_data in enumerate(result["figures"]):
            fig_type = fig_data.get("type", "").lower()
            dimensions = fig_data.get("dimensions", [])
            offset = fig_data.get("offset", [0.0, 0.0, 0.0])
            rotation = fig_data.get("rotation", [0.0, 0.0, 0.0])
            color = FIGURE_COLORS[idx % len(FIGURE_COLORS)]

            primitives.append({
                "type": fig_type,
                "dimensions": dimensions,
                "offset": offset,
                "rotation": rotation,
                "color": color,
            })

        if not primitives:
            return Response(e("E.20"), status=400)

        return Response(primitives, status=200)
