from .base_figure import BaseFigure


class PyramidFigure(BaseFigure):

    def generate_figure(self):
        if len(self.dimensions) != 3:
            raise ValueError("Dimensions must have length 3.")

        l, w, h = self.dimensions[0], self.dimensions[1], self.dimensions[2]

        self.vertices = [
            [0, 0, 0],      
            [l, 0, 0],        
            [l, w, 0],      
            [0, w, 0],         
            [l / 2, w / 2, h],  
        ]

        self.faces = [
            [0, 3, 2, 1],  
            [0, 1, 4],    
            [1, 2, 4],   
            [2, 3, 4],    
            [3, 0, 4],    
        ]
