import cv2


def embed_message(channel, message, offset, length):
    rows, cols = channel.shape

    bits_per_row = int(length * 8 / rows)

    space_between_bits = int(cols / bits_per_row)

    actual_message = message[offset:offset + length]

    outer_index = inner_index = 0

    for i in range(0, rows):
        for j in range(0, cols, space_between_bits):
            if outer_index == length:
                break

            current_letter_bits = bin(actual_message[outer_index])[2:].zfill(8)
            current_letter_bit = current_letter_bits[inner_index]

            current_channel_bits = bin(channel[i][j])[2:].zfill(8)
            current_channel_bits = current_channel_bits[:7] + current_letter_bit

            channel[i][j] = int(current_channel_bits, 2)

            if inner_index == 7:
                inner_index = 0
                outer_index += 1
            else:
                inner_index += 1

    print("Inserted {} bytes".format(length))
    print("Current offset inside message {}\n".format(outer_index))


def main():
    with open("lorem", "rb") as lorem_file:
        lorem = lorem_file.read()

    img = cv2.imread("checkerboard.bmp", cv2.IMREAD_COLOR)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    if len(lorem) * 8 > img.size * 0.8:
        print("Cover image is not big enough")
        exit(-1)

    one_third_size = int(len(lorem) / 3)

    green_message_size = blue_message_size = one_third_size
    red_message_size = one_third_size if len(lorem) % 3 == 0 else one_third_size + (len(lorem) % 3)

    embed_message(img[:, :, 0], lorem, 0, red_message_size)
    embed_message(img[:, :, 1], lorem, red_message_size, green_message_size)
    embed_message(img[:, :, 2], lorem, red_message_size + green_message_size, blue_message_size)

    img = cv2.cvtColor(img, cv2.COLOR_RGB2BGR)
    cv2.imwrite("lsb_result.bmp", img)


if __name__ == "__main__":
    main()
