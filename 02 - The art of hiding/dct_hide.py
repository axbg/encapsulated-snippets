import cv2
import scipy


def embed_channel(channel, hidden_channel, norm):
    height, width = channel.shape
    hidden_channel_list = [item for sublist in hidden_channel.tolist() for item in sublist]

    outer_index = 0
    for i in range(0, height, 8):
        for j in range(0, width, 8):
            if j + 7 >= width or outer_index == len(hidden_channel_list):
                break

            dct_block = scipy.fft.dct(scipy.fft.dct(channel[i:i + 8, j:j + 8].T, norm='ortho').T, norm='ortho')

            dct_block[4][4] = (hidden_channel_list[outer_index] - 127) * norm

            channel[i:i + 8, j:j + 8] = scipy.fft.idct(scipy.fft.idct(dct_block.T, norm='ortho').T, norm='ortho')
            outer_index += 1


def main():
    img = cv2.imread("cover.bmp", cv2.IMREAD_COLOR)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    secret_image = cv2.imread("secret.bmp", cv2.IMREAD_COLOR)
    secret_image = cv2.cvtColor(secret_image, cv2.COLOR_BGR2RGB)

    cover_height, cover_width, _ = img.shape

    possible_matrices = int(cover_width / 8) * int(cover_height / 8)

    if possible_matrices < secret_image.size / 3:
        print("Secret image is too big for the cover image you want to use")
        exit(-1)

    norm = 0.8
    embed_channel(img[:, :, 0], secret_image[:, :, 0], norm)
    embed_channel(img[:, :, 1], secret_image[:, :, 1], norm)
    embed_channel(img[:, :, 2], secret_image[:, :, 2], norm)

    retrieved_secret_image = cv2.cvtColor(img, cv2.COLOR_RGB2BGR)
    cv2.imwrite("embedded_cover.bmp", retrieved_secret_image)


if __name__ == "__main__":
    main()
