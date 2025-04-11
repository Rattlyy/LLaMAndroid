<p align="center">
<img width="500px" height="300px" src="https://github.com/user-attachments/assets/d637c667-18ab-4689-955d-2b7f52ff4ff6" alt="header"></img>
</p>

<hr/>

# LLaMAndroid

LLaMAndroid is a simple Android app that allows you to run LLaMA models on your phone using the
llama.cpp library. 

This is a PoC to show that it is possible to run LLaMA models on Android devices.

## Requirements
- Android 12.0 or higher
- Adreno GPU (Snapdragon 7 recommended)
- As much RAM as possible (>8GB recommended)

## Tested Devices
| Device               | RAM  | Processor          | GPU        |
|----------------------|------|--------------------|------------|
| Motorola Edge 50 Pro | 12GB | Snapdragon 7 Gen 3 | Adreno 720 |

# Models
The app currently supports the following models:
- Phi 125k Q4_0
- DeepSeek R1 1.5B Q4_0 

# Credits
[java-llama.cpp](https://github.com/kherud/java-llama.cpp) - Java bindings for llama.cpp
