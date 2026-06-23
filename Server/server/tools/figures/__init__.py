from .cuboid import CuboidFigure
from .cylinder import CylinderFigure
from .cone import ConeFigure
from .sphere import SphereFigure
from .pyramid import PyramidFigure
from .prism import PrismFigure
from .torus import TorusFigure


figures_dict = {
    "cuboid": CuboidFigure,
    "cube": CuboidFigure,
    "box": CuboidFigure,
    "rectang": CuboidFigure,
    "rectangle": CuboidFigure,

    "cylinder": CylinderFigure,

    "cone": ConeFigure,

    "sphere": SphereFigure,
    "ball": SphereFigure,

    "pyramid": PyramidFigure,

    "triangular_prism": lambda dims: PrismFigure(dims, sides=3),
    "hexagonal_prism": lambda dims: PrismFigure(dims, sides=6),
    "prism": PrismFigure,

    "torus": TorusFigure,
    "ring": TorusFigure,
    "donut": TorusFigure,
}
