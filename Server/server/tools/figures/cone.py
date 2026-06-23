from .base_figure import BaseFigure
import numpy as np


class ConeFigure(BaseFigure):

    def __init__(self, dimensions, segments=32):
        self.segments = segments
        super().__init__(dimensions)

    def generate_figure(self):
        if len(self.dimensions) != 2:
            raise ValueError("Dimensions must have length 2.")

        radius = self.dimensions[0]
        height = self.dimensions[1]
        n = self.segments

        for i in range(n):
            theta = 2 * np.pi * i / n
            self.vertices.append([radius * np.cos(theta), radius * np.sin(theta), 0])

        apex_idx = n
        self.vertices.append([0, 0, height])

        # Base face → normal -z (outward from bottom)
        self.faces.append(list(range(n))[::-1])

        # Side triangles
        for i in range(n):
            next_i = (i + 1) % n
            self.faces.append([i, next_i, apex_idx])
