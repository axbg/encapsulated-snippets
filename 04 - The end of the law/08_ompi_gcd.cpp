#include <iostream>
#include <mpi.h>

using namespace std;

int* generateRandomNumbers(int n) {
    if (n > 0) {
        int* numbers = new int[n];

        for (int i = 0; i < n; i++) {
            numbers[i] = rand() + 1;
        }

        return numbers;
    }

    return NULL;
}

int gcd(int a, int b) {
    int r = a % b;

    while (r != 0) {
        a = b;
        b = r;
        r = a % b;
    }

    return b;
}

int* sequentialGcd(int* numbers, int length) {
    int* results = new int[length / 2];

    for (int i = 0, j = 0; i < length; i += 2, j++) {
        results[j] = gcd(numbers[i], numbers[i + 1]);
    }

    return results;
}

int main(int argc, char** argv)
{
    // length should be divisible with the worldSize - the number of nodes
    int worldRank, worldSize, length = 100000;
    int* numbers, * results;

    MPI_Init(&argc, &argv);
    MPI_Comm_size(MPI_COMM_WORLD, &worldSize);
    MPI_Comm_rank(MPI_COMM_WORLD, &worldRank);

    int nodeLength = length / worldSize;

    // because each array of inputs will result in an array half its size
    int resultLength = length / 2;

    // the random numbers will be generated only on the central node
    if (worldRank == 0) {
        numbers = generateRandomNumbers(length);
        results = new int[resultLength];
    }

    int* receivedNumbers = new int[nodeLength];

    // scatter the data to all of the nodes
    MPI_Scatter(numbers, nodeLength, MPI_INT, receivedNumbers, nodeLength, MPI_INT, 0, MPI_COMM_WORLD);

    // compute gcd for the received chunk of numbers
    int* gcdResults = sequentialGcd(receivedNumbers, nodeLength);

    // gather the data from all the nodes to the central node
    MPI_Gather(gcdResults, nodeLength / 2, MPI_INT, results, nodeLength / 2, MPI_INT, 0, MPI_COMM_WORLD);

    delete[] gcdResults;
    delete[] receivedNumbers;

    MPI_Finalize();

    if (worldRank == 0) {
        for (int i = 0; i < resultLength; i++) {
            printf("%d\n", results[i]);
        }

        delete[] results;
        delete[] numbers;
    }

    return 0;
}