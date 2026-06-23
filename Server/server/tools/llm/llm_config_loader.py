import os

CONFIG = {
    "ollama": {
        "base_url": "http://host.docker.internal:11434",
    },
    "models": {
        "simple": {
            "name": "gemma3:4b",
            "timeout_seconds": 120,
            "prompt_template": (
                "You are analyzing a hand-drawn engineering drawing to extract 3D shape data. "
                "Find ONLY the ONE main 3D primitive shown in the drawing.\n\n"
                "PRIMITIVES — dimensions order:\n"
                "  cuboid            — dimensions=[length, width, height]\n"
                "  cylinder          — dimensions=[radius, height]\n"
                "  cone              — dimensions=[radius, height]\n"
                "  sphere            — dimensions=[radius]\n"
                "  pyramid           — dimensions=[base left-right, base depth, base→apex]\n"
                "  prism             — dimensions=[circumradius of hexagon, length along axis]\n"
                "  triangular_prism  — dimensions=[circumradius of triangle, length along axis]\n"
                "  torus             — dimensions=[major_radius, minor_radius]\n\n"
                "RULES:\n"
                "The user message contains EXACTLY the dimension values written on the drawing. "
                "You MUST use ALL of them in the dimensions array. Never discard a number.\n"
                "Do not make up sizes — read them strictly from the drawing.\n"
                "All values in dimensions must be ONLY numbers — no text, no units.\n\n"
                "Output format — a single JSON object:\n"
                '{"type":"cuboid","dimensions":[120,50,30]}\n\n'
                "Return ONLY this JSON object, nothing else."
            ),
        },
        "ocr": {
            "name": "qwen2.5vl:7b",
            "timeout_seconds": 60,
            "prompt_template": (
                "You are an OCR tool. Look at this engineering drawing. "
                "Find and return EVERY number written anywhere in the image. "
                "Return ONLY a comma-separated list of numbers. No text, no explanation. "
                "Example: 5, 10, 30, 120"
            ),
        },
        "advanced": {
            "name": "gemma3:12b",
            "timeout_seconds": 180,
            "prompt_template": (
                "You are analyzing a hand-drawn engineering drawing to extract 3D shape data. You need find ALL PRIMITIVES its may be more then 1."
                "First, think about how many of the figures below are in the picture and what is this figure"
                "\n\n"
                "PRIMITIVES — dimensions=[length, width, height] unless noted:\n"
                "  cuboid            — dimensions=[length, width, height]\n"
                "  cylinder          — dimensions=[radius, height]\n"
                "  cone              — dimensions=[radius, height]\n"
                "  sphere            — dimensions=[radius]\n"
                "  pyramid           — dimensions=[base left-right, base depth, base→apex]\n"
                "  prism             — dimensions=[circumradius of hexagon, length along axis]\n"
                "  triangular_prism  — dimensions=[circumradius of triangle, length along axis]\n"
                "  torus             — dimensions=[major_radius, minor_radius]\n\n"
                "RULES:\n"
                "The user message contains THE COMPLETE LIST of every number written on the drawing — there are no other numbers. "
                "You MUST use ALL of them — each number must appear somewhere in dimensions arrays. Never discard a number.\n"
                "FORBIDDEN: do not use any number that is not in the provided list. "
                "Do not invent, estimate, or derive new values. Every dimension must be taken exactly from the list.\n"
                "Each primitive has its OWN dimensions read from its own annotation arrows — never copy dimensions between primitives.\n"
                "If two dimensions appear to have the same value, verify carefully — length and depth are rarely equal in perspective drawings.\n"
                "All arrays (dimensions, offset, rotation) must contain ONLY numbers — no text, no units, no comments.\n"
                "If a dimension value truly cannot be read from the drawing, use 0 as a placeholder.\n"
                "OFFSET: primitives stack in space — they must NOT overlap. "
                "If one figure sits on top of another, set its offset[2] (Z) equal to the height of the figure below it. "
                "If figures are side by side, use offset[0] or offset[1] accordingly.\n\n"
                "Example:\n"
                '[{"type":"cuboid","dimensions":[5,3,8],"offset":[0,0,0],"rotation":[0,0,0]},'                    
                ' {"type":"pyramid","dimensions":[6,3,4],"offset":[0,0,8],"rotation":[0,0,0]}]\n\n'                    
                "Output format — one object per body, single flat array, no extra brackets.\n"
                "Return ONLY this JSON array, nothing else."
            ),
        },
        "offset": {
            "name": "gemma3:4b",
            "timeout_seconds": 60,
            "prompt_template": (
                "You are positioning 3D figures from a hand-drawn engineering drawing in 3D space.\n"
                "The user message contains a list of figures with their types and dimensions — "
                "the dimensions are already correct, do NOT change them.\n"
                "Your ONLY task is to determine the correct offset [x, y, z] for each figure and "
                "reorder them from BOTTOM to TOP as they appear in the drawing.\n\n"
                "OFFSET RULES:\n"
                "  - The bottommost figure always has offset [0, 0, 0]\n"
                "  - Stacked vertically: each figure's offset[2] = sum of heights of all figures below it\n"
                "  - Side by side: use offset[0] (X axis) or offset[1] (Y axis)\n"
                "  - Figures NEVER overlap\n\n"
                "Do NOT change type or dimensions. Do NOT add or remove figures.\n\n"
                "Return ONLY a JSON array with figures reordered bottom-to-top and offset added.\n"
                "Example input: [{'type':'pyramid','dimensions':[6,3,4]}, {'type':'cuboid','dimensions':[5,3,8]}]\n"
                'Example output: [{"type":"cuboid","dimensions":[5,3,8],"offset":[0,0,0]},{"type":"pyramid","dimensions":[6,3,4],"offset":[0,0,8]}]\n'
                "Return ONLY the JSON array, nothing else."
            ),
        },
    },
}


class LLMConfig:

    @classmethod
    def get(cls) -> dict:
        return CONFIG

    @classmethod
    def get_model(cls, mode: str) -> dict:
        return CONFIG["models"][mode]

    @classmethod
    def get_ollama_url(cls) -> str:
        return CONFIG["ollama"]["base_url"]
