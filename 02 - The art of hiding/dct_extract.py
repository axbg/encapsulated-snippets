import cv2
import numpy as np
import scipy
from matplotlib import pyplot as plt


def decode_channel(channel, size, norm):
    height, width = channel.shape

    pixels = []
    outer_index = 0
    for i in range(0, height, 8):
        for j in range(0, width, 8):
            if j + 7 >= width or outer_index == size:
                break
            dct_block = scipy.fft.dct(scipy.fft.dct(channel[i:i + 8, j:j + 8].T, norm='ortho').T, norm='ortho')

            pixels.append(int(dct_block[4][4] / norm) + 127)
            outer_index += 1

    return pixels


def main():
    # we need to know the secret's shape and the norm
    secret_height = 100
    secret_width = 100
    norm = 0.8

    embedded_image = cv2.imread("embedded_cover.bmp", cv2.IMREAD_COLOR)
    embedded_image = cv2.cvtColor(embedded_image, cv2.COLOR_BGR2RGB)

    red_pixels = decode_channel(embedded_image[:, :, 0], secret_height * secret_width, norm)
    green_pixels = decode_channel(embedded_image[:, :, 1], secret_height * secret_width, norm)
    blue_pixels = decode_channel(embedded_image[:, :, 2], secret_height * secret_width, norm)

    retrieved_secret_image = np.zeros((secret_height, secret_width, 3), np.uint8)

    current_index = 0
    for i in range(0, secret_height):
        for j in range(0, secret_width):
            retrieved_secret_image[i][j][0] = int(red_pixels[current_index])
            retrieved_secret_image[i][j][1] = int(green_pixels[current_index])
            retrieved_secret_image[i][j][2] = int(blue_pixels[current_index])
            current_index += 1

    plt.imshow(retrieved_secret_image)
    plt.show()


if __name__ == "__main__":
    main()
