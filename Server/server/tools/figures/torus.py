from .base_figure import BaseFigure
import numpy as np


class TorusFigure(BaseFigure):

    def __init__(self, dimensions, major_segments=32, minor_segments=16):
        self.major_segments = major_segments
        self.minor_segments = minor_segments
        super().__init__(dimensions)

    def generate_figure(self):
        if len(self.dimensions) != 2:
            raise ValueError("Dimensions must have length 2.")

        R = self.dimensions[0] 
        r = self.dimensions[1]  
        M = self.major_segments
        N = self.minor_segments

        for i in range(M):
            phi = 2 * np.pi * i / M
            for j in range(N):
                theta = 2 * np.pi * j / N
                self.vertices.append([
                    (R + r * np.cos(theta)) * np.cos(phi),
                    (R + r * np.cos(theta)) * np.sin(phi),
                    r * np.sin(theta),
                ])

        for i in range(M):
            next_i = (i + 1) % M
            for j in range(N):
                next_j = (j + 1) % N
                a = i * N + j
                b = i * N + next_j
                c = next_i * N + next_j
                d = next_i * N + j
                self.faces.append([a, b, c, d])
