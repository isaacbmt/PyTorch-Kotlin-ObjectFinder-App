import os
import torch
from PIL import Image
from torch.utils.mobile_optimizer import optimize_for_mobile
from torchvision import transforms
import matplotlib.pyplot as plt
from pascal_dict import classes


# Pretrained model modified to be used directly in android
class DeepLabAndroid(torch.nn.Module):
    def __init__(self, model) -> None:
        super(DeepLabAndroid, self).__init__()
        self.model = model
        self.classes = classes

    def forward(self, x):
        x = self.model(x)["out"][0]
        x = torch.argmax(x, 0)
        #ones = torch.ones(x.shape)
        #x = x * ones
        x = x.cpu().data
        labels = torch.unique(x)
        for l in labels:
            x[x == l] = self.classes[l]
        x = x.squeeze(0)
        return x        


project_path = os.path.dirname(os.path.realpath(__file__))
project_path = os.path.join(project_path, "app/src/main/assets")
model = torch.hub.load('pytorch/vision:v0.13.1', 'deeplabv3_resnet50', pretrained=True)
# Set grad to false 
model.eval()

aModel = DeepLabAndroid(model)

script_module = torch.jit.script(aModel)
opt_script_module = optimize_for_mobile(script_module)
opt_script_module._save_for_lite_interpreter(os.path.join(project_path, "deeplabv3_model_optimized_m.ptl"))

"""Test Model
input_image = Image.open(r"C:/Users/XT/Downloads/p1.jpg")
width, height = input_image.width, input_image.height
preprocess = transforms.Compose([
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
])

input_tensor = preprocess(input_image)
input_batch = input_tensor.unsqueeze(0)


output = model(input_batch)["out"][0]
print(output.dtype)
output = torch.argmax(output, 0)#.squeeze(0)
print(output.dtype)
print("output shape: ", output.shape)
print(output)
plt.imshow(output)
plt.show()
#"""
