<p align="center">
<img width="500px" height="300px" src="https://github.com/user-attachments/assets/d637c667-18ab-4689-955d-2b7f52ff4ff6" alt="header"></img>
</p>

<hr/>

# LLaMAndroid

LLaMAndroid is a simple Android app that allows you to run LLaMA models on your phone using the
llama.cpp library. 

This is a PoC to show that it is possible to run LLaMA models on Android devices.
Currently performance is not optimal and the most it can do is run small models.
Hopefully in the next years we will see more powerful phones that can run larger models.

## Requirements
- Android 9 or higher
- Adreno GPU (>=Adreno 7xx)
- As much RAM as possible (>8GB recommended)

## Tested Devices
| Device               | RAM  | Processor          | GPU        | Avg T/s |
|----------------------|------|--------------------|------------|---------|
| Motorola Edge 50 Pro | 12GB | Snapdragon 7 Gen 3 | Adreno 720 | 6 T/s   |
| Sony Xperia 1 V      | 12GB | Snapdragon 8 Gen 2 | Adreno 740 | 9 T/s   |

# Models
The app currently supports the following models: (llama.cpp only supports Q4_0 models)
- Phi 128k 
- DeepSeek R1 1.5B -- Distilled from Qwen
- LLaMA 3.2 1B
- Gemma3 1B

# Credits
[java-llama.cpp](https://github.com/kherud/java-llama.cpp) - Java bindings for llama.cpp
