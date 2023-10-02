#define _GNU_SOURCE

#include "kaffinity.h"

int set_affinity(pthread_t thread, cpu_set_t* set) {
    return pthread_setaffinity_np(thread, sizeof(*set), set);
}
int get_affinity(pthread_t thread, cpu_set_t* set) {
    return pthread_getaffinity_np(thread, sizeof(*set), set);
}

void cpu_zero(cpu_set_t* set) {
    CPU_ZERO(set);
}
void cpu_set(int cpu, cpu_set_t* set) {
    CPU_SET(cpu, set);
}
int cpu_isset(int cpu, cpu_set_t* set) {
    CPU_ISSET(cpu, set);
}
int cpu_setsize() {
    return CPU_SETSIZE; 
}
