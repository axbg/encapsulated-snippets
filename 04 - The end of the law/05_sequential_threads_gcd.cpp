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

void executeAndBenchmark(const char* name, void (*function)(int*, int), int* numbers, int length) {
    auto start = chrono::system_clock::now();

    function(numbers, length);

    auto end = chrono::system_clock::now();
    chrono::duration<double> elapsed = end - start;
    printf("%s GCD finished in %f\n", name, elapsed.count());
}

void sequentialGcd(int* numbers, int length) {
    int* results = new int[length / 2];

    for (int i = 0, j = 0; i < length; i += 2, j++) {
        results[j] = gcd(numbers[i], numbers[i + 1]);
    }

    delete[] results;
}

void threadsGcd(int* numbers, int length) {
    int* firstHalf = new int[length / 2];
    int* secondHalf = new int[length / 2];

    for (int i = 0; i < length / 2; i++) {
        firstHalf[i] = numbers[i];
    }

    for (int j = 0, i = length / 2; i < length; i++) {
        secondHalf[j] = numbers[i];
    }

    // we can use futures to get a pointer to the results on each thread
    // but it's not the scope of the example
    thread t1 = thread(sequentialGcd, ref(firstHalf), length / 2);
    thread t2 = thread(sequentialGcd, ref(secondHalf), length / 2);

    t1.join();
    t2.join();

    delete[] firstHalf;
    delete[] secondHalf;
}


int main()
{
    int length = 100000;
    int* numbers = generateRandomNumbers(length);

    executeAndBenchmark("Sequential", sequentialGcd, numbers, length);
    executeAndBenchmark("Threads", threadsGcd, numbers, length);

    delete[] numbers;
    return 0;
}