import numpy as np
from tools.mesh_utils import triangulate_faces


class BaseFigure:
    def __init__(self, dimensions):
        self.dimensions = dimensions
        self.vertices = []
        self.faces = []
        self.generate_figure()

    def generate_figure(self):
        pass

    def apply_offset(self, offset):
        ox, oy, oz = offset[0], offset[1], offset[2]
        self.vertices = [[v[0] + ox, v[1] + oy, v[2] + oz] for v in self.vertices]
        return self

    def apply_rotation(self, angles_deg):
        rx = np.radians(angles_deg[0])
        ry = np.radians(angles_deg[1])
        rz = np.radians(angles_deg[2])
        Rx = np.array([[1, 0, 0], [0, np.cos(rx), -np.sin(rx)], [0, np.sin(rx), np.cos(rx)]])
        Ry = np.array([[np.cos(ry), 0, np.sin(ry)], [0, 1, 0], [-np.sin(ry), 0, np.cos(ry)]])
        Rz = np.array([[np.cos(rz), -np.sin(rz), 0], [np.sin(rz), np.cos(rz), 0], [0, 0, 1]])
        R = Rz @ Ry @ Rx
        self.vertices = [list(R @ np.array(v)) for v in self.vertices]
        return self

    def to_obj_string(self) -> str:
        lines = [f"v {v[0]} {v[1]} {v[2]}" for v in self.vertices]
        lines.append("")
        lines += ["f " + " ".join(str(i + 1) for i in face) for face in triangulate_faces(self.faces)]
        return "\n".join(lines)

    def save_as_obj(self, path: str):
        with open(path, "w") as f:
            f.write(self.to_obj_string())
