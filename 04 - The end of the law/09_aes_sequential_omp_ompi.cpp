#include<iostream>
#include<omp.h>
#include<mpi.h>
#include "aes.c"

using namespace std;

// the structure that will hold 16 bytes of data which will be encrypted at once
// it also has a flag to announce if the current block is the last one
// based on that we will calculate and apply, or not, our PKCS7 padding
struct Block {
    uint8_t block[16];
    int size;
    bool lastBlock;
};

// initialize a block with size 0 and lastBlock flag to false
Block initBlock() {
    Block block;

    block.size = 0;
    memset(block.block, 0x00, 16);
    block.lastBlock = false;

    return block;
}

// parse through all blocks required to store the content to be encrypted and initialize them
Block* initBlocks(const int numberOfBlocks) {
    Block* blocks = new Block[numberOfBlocks];
    for (int i = 0; i < numberOfBlocks; i++) {
        blocks[i] = initBlock();
    }

    return blocks;
}

// print the content of a block in hexadecimal format
void printBlock(Block block) {
    for (int i = 0; i < block.size; i++) {
        printf("%02X ", block.block[i]);
    }
}

// parse through all the blocks and print each one
void printBlocks(Block* blocks, int numberOfBlocks) {
    for (int i = 0; i < numberOfBlocks; i++) {
        printBlock(blocks[i]);
    }

    printf("\n");
}

// the implementation of PKCS7 padding
// should be applied only on the last block
// 0 if no additional block is needed
// 1 if additional block is needed
int pkcs7pad(Block* block) {
    if (block->size == 16) {
        return 1;
    }

    int neededPad = 16 - block->size;
    int i = 0;
    int j = 15;

    while (i < neededPad) {
        block->block[j] = neededPad;
        i++;
        j--;
    }

    block->size = 16;
    return 0;
}

// encrypting blocks using AES in a sequential way
void sequential_AES_encrypt(Block* blocks, int size, AES_ctx ctx) {
    for (int i = size - 1; i >= 0; i--) {
        if (blocks[i].lastBlock) {
            int nextBlockNeeded = pkcs7pad(&blocks[i]);

            if (nextBlockNeeded == 1) {
                blocks[i + 1].lastBlock = true;
                blocks[i + 1].size = 16;
                memset(blocks[i + 1].block, 0x10, 16);
            }

            break;
        }
    }

    for (int i = 0; i < size; i++) {
        AES_ECB_encrypt(&ctx, blocks[i].block);
    }
}

// encrypting blocks using AES in a parallel way using OpenMP
void parallel_AES_encrypt(Block* blocks, int size, AES_ctx ctx) {
    for (int i = size - 1; i >= 0; i--) {
        if (blocks[i].lastBlock) {
            int nextBlockNeeded = pkcs7pad(&blocks[i]);

            if (nextBlockNeeded == 1) {
                blocks[i + 1].lastBlock = true;
                blocks[i + 1].size = 16;
                memset(blocks[i + 1].block, 0x10, 16);
            }

            break;
        }
    }

    omp_set_num_threads(omp_get_num_procs());

# pragma omp parallel for shared(blocks, size)
    for (int i = 0; i < size; i++) {
        AES_ECB_encrypt(&ctx, blocks[i].block);
    }
}

// save the encryption result to file
void saveToFile(Block* blocks, int size, char* filename) {
    FILE* file = fopen(filename, "wb+");

    if (file) {
        for (int i = 0; i < size; i++) {
            fwrite(blocks[i].block, sizeof(uint8_t), blocks[i].size, file);
        }

        fclose(file);
    }

    printf("Encrypted file was saved in %s\n", filename);
}

int main(int argc, char** argv) {
    int worldRank, worldSize;

    MPI_Init(&argc, &argv);
    MPI_Comm_size(MPI_COMM_WORLD, &worldSize);
    MPI_Comm_rank(MPI_COMM_WORLD, &worldRank);

    char password[16];
    int numberOfBlocks = 0;
    Block* sequentialBlocks;
    Block* distributedBlocks;
    Block* parallelBlocks;

    // preparing the arrays which will hold the number of the bytes sent to each node
    // and the displacement of data in the initial array for each node
    int* sendCounts = new int[worldSize];
    int* displs = new int[worldSize];

    // this block of code runs only on the principal node
    if (worldRank == 0) {
        if (argc != 3) {
            printf("The program requires 2 arguments: the absolut path of the file to be encrypted, and the password!\n");
            printf("The password must have 16 characters\n");

            exit(1);
        }

        char* filename = argv[1];
        strcpy(password, argv[2]);

        // preparing the AES secret key
        AES_ctx ctx;
        uint8_t key[16];
        memcpy(key, password, 16);
        AES_init_ctx(&ctx, key);

        numberOfBlocks = 0;

        // reading the input string and splits it into the right number of blocks
        FILE* f = fopen(filename, "rb+");
        int filesize = 0;

        if (!f) {
            printf("The file was not found\n");
            exit(1);
        }

        fseek(f, 0, SEEK_END);
        filesize = ftell(f);
        fseek(f, 0, SEEK_SET);

        numberOfBlocks = filesize / 16 + 1;
        sequentialBlocks = initBlocks(numberOfBlocks);
        parallelBlocks = initBlocks(numberOfBlocks);
        distributedBlocks = initBlocks(numberOfBlocks);


        // populating the blocks with the input data
        int i = 0;
        int bytesRead = 0;
        while ((bytesRead = fread(sequentialBlocks[i].block, sizeof(char), 16, f)) > 0) {
            memcpy(parallelBlocks[i].block, sequentialBlocks[i].block, bytesRead);
            memcpy(distributedBlocks[i].block, sequentialBlocks[i].block, bytesRead);
            sequentialBlocks[i].size = bytesRead;
            parallelBlocks[i].size = bytesRead;
            distributedBlocks[i].size = bytesRead;
            i++;
        }

        // marking the last blocks
        sequentialBlocks[i - 1].lastBlock = true;
        parallelBlocks[i - 1].lastBlock = true;
        distributedBlocks[i - 1].lastBlock = true;
        fclose(f);

        // executing AES sequentially and in parallel and benchmark using OMP methods
        double sequentialStart = omp_get_wtime();
        sequential_AES_encrypt(sequentialBlocks, numberOfBlocks, ctx);
        double sequentialEnd = omp_get_wtime();

        double parallelStart = omp_get_wtime();
        parallel_AES_encrypt(parallelBlocks, numberOfBlocks, ctx);
        double parallelEnd = omp_get_wtime();

        // printing the results of the benchmarks
        printf("Sequential AES encryption was executed in %5.5f seconds\n", (sequentialEnd - sequentialStart));
        printf("Parallel AES encryption was executed in %5.5f seconds\n", (parallelEnd - parallelStart));

        // computing how much bytes will receive each distributed node
        int sum = 0;
        for (int i = 0; i < worldSize; i++) {
            int recv = numberOfBlocks / worldSize;

            if (i == numberOfBlocks % worldSize) {
                recv += numberOfBlocks % worldSize;
            }

            sendCounts[i] = recv * sizeof(Block);
            displs[i] = sum * sizeof(Block);
            sum += recv;
        }
    }

    AES_ctx ctx1;
    uint8_t key[16];
    double distributedStart, distributedEnd;

    // starting the distributed benchmark and passing the key to all the nodes
    distributedStart = omp_get_wtime();
    MPI_Bcast(&password, 16, MPI_BYTE, 0, MPI_COMM_WORLD);
    memcpy(key, password, 16);
    AES_init_ctx(&ctx1, key);

    // passing the number of blocks for each node
    MPI_Bcast(&numberOfBlocks, 1, MPI_INT, 0, MPI_COMM_WORLD);

    int numberOfReceivedBlocks = numberOfBlocks / worldSize;
    if (numberOfBlocks % worldSize == worldRank) {
        numberOfReceivedBlocks += numberOfBlocks % worldSize;
    }

    Block* receivedBlocks = new Block[numberOfReceivedBlocks];

    // passing the blocks of data to each node, according to a displacement
    MPI_Scatterv(distributedBlocks, sendCounts, displs, MPI_BYTE, receivedBlocks, numberOfReceivedBlocks * sizeof(Block), MPI_BYTE, 0, MPI_COMM_WORLD);

    // encrypting the received blocks in parallel on each node
    parallel_AES_encrypt(receivedBlocks, numberOfReceivedBlocks, ctx1);

    // receiving the encrypted blocks from all the nodes to the central node
    MPI_Gatherv(receivedBlocks, numberOfReceivedBlocks * sizeof(Block), MPI_BYTE, distributedBlocks, sendCounts, displs, MPI_BYTE, 0, MPI_COMM_WORLD);
    distributedEnd = omp_get_wtime();

    delete[] displs;
    delete[] sendCounts;
    delete[] receivedBlocks;

    MPI_Finalize();

    // printing the result of the distributed benchmark and saving all the results to a file
    if (worldRank == 0) {
        printf("Distributed + Parallel AES encryption was executed in %5.5f seconds\n\n", (distributedEnd - distributedStart));

        saveToFile(sequentialBlocks, numberOfBlocks, "sequentially_encrypted_file");
        saveToFile(parallelBlocks, numberOfBlocks, "parallel_encrypted_file");
        saveToFile(distributedBlocks, numberOfBlocks, "distributed_encrypted_file");

        delete[] sequentialBlocks;
        delete[] parallelBlocks;
        delete[] distributedBlocks;
    }

    return 0;
}