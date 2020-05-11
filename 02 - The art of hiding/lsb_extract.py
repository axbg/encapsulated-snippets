import cv2


def extract_message(channel, length):
    hidden_message = []
    rows, cols = channel.shape
    bits_per_row = int(length * 8 / rows)
    space_between_bits = int(cols / bits_per_row)

    inner_index = 0
    outer_index = 0
    extracted_bits = 0

    for i in range(0, rows):
        for j in range(0, cols, space_between_bits):
            if outer_index == length:
                break

            current_channel_bits = bin(channel[i][j])[2:].zfill(8)
            hidden_message.append(current_channel_bits[7])

            if inner_index == 7:
                inner_index = 0
                outer_index += 1
            else:
                inner_index += 1

            extracted_bits += 1

    print("Extracted {} bytes".format(extracted_bits / 8))
    print("Current offset inside message {}\n".format(outer_index))

    return hidden_message


def main():
    # we need to know the size of the message
    lorem = 6455

    img = cv2.imread("lsb_result.bmp", cv2.IMREAD_COLOR)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    red_channel = img[:, :, 0]
    green_channel = img[:, :, 1]
    blue_channel = img[:, :, 2]

    if lorem * 8 > img.size:
        print("Image is too small to hide a message with this size")
        exit(-1)

    one_third_size = int(lorem / 3)

    if lorem % 3 == 0:
        red_message_size = green_message_size = blue_message_size = one_third_size
    else:
        red_message_size = one_third_size + (lorem % 3)
        green_message_size = blue_message_size = one_third_size

    red_message = extract_message(red_channel, red_message_size)
    green_message = extract_message(green_channel, green_message_size)
    blue_message = extract_message(blue_channel, blue_message_size)

    full_message = []
    full_message.extend(red_message)
    full_message.extend(green_message)
    full_message.extend(blue_message)

    clear_message = ""
    for i in range(0, len(full_message), 8):
        clear_message += chr(int(''.join(full_message[i:i + 8]), 2))

    print(clear_message)


if __name__ == "__main__":
    main()
