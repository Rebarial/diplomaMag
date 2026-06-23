from .base_figure import BaseFigure
import numpy as np


class PrismFigure(BaseFigure):

    def __init__(self, dimensions, sides=6):
        self.sides = sides
        super().__init__(dimensions)

    def generate_figure(self):
        if len(self.dimensions) != 2:
            raise ValueError("Dimensions must have length 2.")

        radius = self.dimensions[0]
        height = self.dimensions[1]
        n = self.sides

        for i in range(n):
            theta = 2 * np.pi * i / n
            self.vertices.append([radius * np.cos(theta), radius * np.sin(theta), 0])

        for i in range(n):
            theta = 2 * np.pi * i / n
            self.vertices.append([radius * np.cos(theta), radius * np.sin(theta), height])

        self.faces.append(list(range(n))[::-1])    
        self.faces.append(list(range(n, 2 * n)))      

        for i in range(n):
            next_i = (i + 1) % n
            self.faces.append([i, next_i, next_i + n, i + n])
