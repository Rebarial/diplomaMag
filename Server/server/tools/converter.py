import trimesh
import numpy as np
import os
from trimesh.visual.material import PBRMaterial
from tools.mesh_utils import triangulate_faces


FIGURE_COLORS = [
    [0.85, 0.35, 0.35, 1.0],  # red
    [0.35, 0.60, 0.85, 1.0],  # blue
    [0.35, 0.75, 0.45, 1.0],  # green
    [0.90, 0.72, 0.25, 1.0],  # gold
    [0.70, 0.40, 0.85, 1.0],  # purple
    [0.90, 0.55, 0.25, 1.0],  # orange
    [0.30, 0.80, 0.75, 1.0],  # cyan
    [0.85, 0.40, 0.65, 1.0],  # pink
]

_DEFAULT_MATERIAL = PBRMaterial(
    baseColorFactor=np.array([0.82, 0.78, 0.74, 1.0]),
    roughnessFactor=0.85,
    metallicFactor=0.0,
    doubleSided=True,
)


def _make_material(color: list) -> PBRMaterial:
    return PBRMaterial(
        baseColorFactor=np.array(color),
        roughnessFactor=0.82,
        metallicFactor=0.0,
        doubleSided=True,
    )


def _mesh_from_figure(vertices: list, faces: list) -> trimesh.Trimesh:
    tris = triangulate_faces(faces)
    return trimesh.Trimesh(
        vertices=np.array(vertices, dtype=float),
        faces=np.array(tris, dtype=int),
        process=False,
    )


def obj_content_to_glb(obj_content: str, tmp_path: str, color: list = None) -> bytes:
    """Single-figure GLB, optionally with a specific color."""
    with open(tmp_path, 'w') as f:
        f.write(obj_content)

    loaded = trimesh.load(tmp_path)
    material = _make_material(color) if color else _DEFAULT_MATERIAL

    if isinstance(loaded, trimesh.Scene):
        for geom in loaded.geometry.values():
            geom.visual.material = material
    elif isinstance(loaded, trimesh.Trimesh):
        loaded.visual.material = material

    glb_path = tmp_path.replace('.obj', '.glb')
    loaded.export(glb_path)

    with open(glb_path, 'rb') as f:
        glb_bytes = f.read()

    os.remove(tmp_path)
    os.remove(glb_path)

    return glb_bytes


def figures_to_glb(figure_data: list) -> bytes:
    """
    Multi-figure GLB with per-figure colors.
    figure_data: list of {'vertices': [...], 'faces': [...], 'color': [r,g,b,a]}
    """
    scene = trimesh.scene.Scene()

    for idx, fig in enumerate(figure_data):
        mesh = _mesh_from_figure(fig['vertices'], fig['faces'])
        color = fig.get('color', FIGURE_COLORS[idx % len(FIGURE_COLORS)])
        mesh.visual.material = _make_material(color)
        scene.add_geometry(mesh, node_name=f'figure_{idx}')

    return scene.export(file_type='glb')
