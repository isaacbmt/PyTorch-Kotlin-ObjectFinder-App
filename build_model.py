import torch
from torch.utils.mobile_optimizer import optimize_for_mobile

model = torch.hub.load('pytorch/vision:v.0.11.0', 'deeplabv3_resnet50', pretrained=True)
model.eval()

