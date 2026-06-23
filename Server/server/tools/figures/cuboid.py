from .base_figure import BaseFigure
import numpy as np

class CuboidFigure(BaseFigure):

    def generate_figure(self):
        if len(self.dimensions) != 3:
            raise ValueError("Dimensions must have length 3.")

        length = self.dimensions[0]
        width = self.dimensions[1]
        height = self.dimensions[2]

        self.vertices = [
            [0, 0, 0],  
            [length, 0, 0], 
            [length, width, 0], 
            [0, width, 0], 
            [0, 0, height],
            [length, 0, height],
            [length, width, height],
            [0, width, height] 
        ]

        self.faces = [
            [0, 1, 2, 3],   
            [4, 7, 6, 5],   
            [0, 4, 5, 1],   
            [2, 6, 7, 3],    
            [1, 5, 6, 2],    
            [3, 7, 4, 0]   
        ]
