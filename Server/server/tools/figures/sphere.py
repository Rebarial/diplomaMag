from .base_figure import BaseFigure
import numpy as np


class SphereFigure(BaseFigure):

    def __init__(self, dimensions, lat_segments=16, lon_segments=32):
        self.lat_segments = lat_segments
        self.lon_segments = lon_segments
        super().__init__(dimensions)

    def generate_figure(self):
        if len(self.dimensions) != 1:
            raise ValueError("Dimensions must have length 1.")

        radius = self.dimensions[0]
        lat_n = self.lat_segments
        lon_n = self.lon_segments

        # North pole (index 0)
        self.vertices.append([0, 0, radius])

        # Latitude rings (indices 1 .. lat_n-1 * lon_n)
        for i in range(1, lat_n):
            phi = np.pi * i / lat_n
            for j in range(lon_n):
                theta = 2 * np.pi * j / lon_n
                self.vertices.append([
                    radius * np.sin(phi) * np.cos(theta),
                    radius * np.sin(phi) * np.sin(theta),
                    radius * np.cos(phi),
                ])

        # South pole (last index)
        self.vertices.append([0, 0, -radius])
        south_idx = len(self.vertices) - 1

        # North cap triangles
        for j in range(lon_n):
            next_j = (j + 1) % lon_n
            self.faces.append([0, j + 1, next_j + 1])

        # Middle quads
        for i in range(lat_n - 2):
            for j in range(lon_n):
                next_j = (j + 1) % lon_n
                a = 1 + i * lon_n + j
                b = 1 + i * lon_n + next_j
                c = 1 + (i + 1) * lon_n + next_j
                d = 1 + (i + 1) * lon_n + j
                self.faces.append([a, b, c, d])

        # South cap triangles
        last_ring = 1 + (lat_n - 2) * lon_n
        for j in range(lon_n):
            next_j = (j + 1) % lon_n
            self.faces.append([south_idx, last_ring + next_j, last_ring + j])
