import numpy as np
from matplotlib import pyplot as plt

ROW_LENGTH = 8
IMAGE_HEIGHT = 8
CHANNELS_NO = 3
DATA_OFFSET = 54
INPUT_IMAGE = "image.bmp"


def main():
    with open("checkerboard.bmp", "rb") as image_file:
        initial_image = image_file.read()

    offset = int.from_bytes(initial_image[10: 13], "little")
    width = int.from_bytes(initial_image[18:21], "little")
    height = int.from_bytes(initial_image[22:25], "little")

    image_data = np.ndarray((width, height, 3), np.uint8)

    r = c = 0
    data_index = offset
    while data_index < len(initial_image):
        image_data[r][c][2] = initial_image[data_index]
        image_data[r][c][1] = initial_image[data_index + 1]
        image_data[r][c][0] = initial_image[data_index + 2]

        c += 1
        data_index += 3
        if c == width:
            c = 0
            r += 1

            data_index += width % 4

    image_data = image_data[::-1]
    plt.imshow(image_data)
    plt.show()



if __name__ == "__main__":
    main()
