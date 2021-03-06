#include <iostream>
#include <thread>
#include <omp.h>

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

void parallelGcd(int* numbers, int length) {
    // we use all the available cores of our processor
    omp_set_num_threads(omp_get_num_procs());

    int* results = new int[length / 2];
    auto start = chrono::system_clock::now();

# pragma omp parallel for shared(numbers, results)
    for (int i = 0; i < length; i += 2) {
        results[i != 0 ? i / 2 : 0] = gcd(numbers[i], numbers[i + 1]);
    }

    auto end = chrono::system_clock::now();
    chrono::duration<double> elapsed = end - start;

    printf("OMP GCD finished in %f\n", elapsed.count());

    delete[] results;
}

void secretSauce(int* numbers, int length) {
    omp_set_num_threads(omp_get_num_procs());

    int** results = new int* [length / 2];
    for (int i = 0; i < length / 2; i++) {
        results[i] = new int[1000];
    }

    auto start = chrono::system_clock::now();

# pragma omp parallel for shared(numbers, results)
    for (int i = 0; i < length; i += 2) {
        results[i != 0 ? i / 2 : 0][0] = gcd(numbers[i], numbers[i + 1]);
    }

    auto end = chrono::system_clock::now();
    chrono::duration<double> elapsed = end - start;

    printf("Secret sauce GCD finished in %f\n", elapsed.count());

    for (int i = 0; i < length / 2; i++) {
        delete[] results[i];
    }

    delete[] results;
}

int main()
{
    int length = 100000;
    int* numbers = generateRandomNumbers(length);

    parallelGcd(numbers, length);
    secretSauce(numbers, length);

    delete[] numbers;
    return 0;
}