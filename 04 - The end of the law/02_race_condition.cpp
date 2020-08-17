#include <iostream>
#include <thread>

using namespace std;

void changeValue(int& n, int newValue) {
    n = newValue;
}

int main()
{
    int numberOfThreads = 10;
    int value = 0;

    thread* threads = new thread[numberOfThreads];

    for (int i = 0; i < numberOfThreads; i++) {
        threads[i] = thread(changeValue,ref(value), i);
    }

    for (int i = 0; i < numberOfThreads; i++) {
        threads[i].join();
    }

    delete[] threads;

    printf("Final result: %d\n", value);
    return 0;
}