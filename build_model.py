import os
import torch
from torch.utils.mobile_optimizer import optimize_for_mobile

project_path = os.path.dirname(os.path.realpath(__file__))


model = torch.hub.load('pytorch/vision:v0.11.0', 'deeplabv3_resnet50', pretrained=True)
# Set the model in validation mode 
model.eval()

script_module = torch.jit.script(model)
opt_script_module = optimize_for_mobile(script_module)
opt_script_module._save_for_lite_interpreter(os.path.join(project_path, "deeplabv3_model_optimized.ptl"))
