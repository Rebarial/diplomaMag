from .base_figure import BaseFigure
import numpy as np

class CylinderFigure(BaseFigure):

    def __init__(self, dimensions, segments=32):
        self.segments=segments
        super().__init__(dimensions)

    def generate_figure(self):
        if len(self.dimensions) != 2:
            raise ValueError("Dimensions must have length 2.")

        radius = self.dimensions[0]
        height = self.dimensions[1]
        segments = self.segments

        for i in range(self.segments):
            theta = 2 * np.pi * i / segments
            x = radius * np.cos(theta)
            y = radius * np.sin(theta)
            self.vertices.append([x, y, 0])

        for i in range(segments):
            theta = 2 * np.pi * i / segments
            x = radius * np.cos(theta)
            y = radius * np.sin(theta)
            self.vertices.append([x, y, height])

        self.faces.append(list(range(segments))[::-1])          # bottom → normal -z
        self.faces.append(list(range(segments, 2 * segments)))  # top    → normal +z
        for i in range(segments):
            next_i = (i + 1) % segments
            self.faces.append([i, next_i, next_i + segments, i + segments])
