#include <iostream>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h> // for fork, pipe
#include <sys/wait.h>

using namespace std;

int main(int argc, char* argv[]) {
    enum {RD, WR}; // pipe fd index RD=0, WR=1
    int fd1[2], fd2[2];
    pid_t pid;

    if (pipe(fd1) < 0) // 1: pipe1 created
        perror("pipe error");
    else if (pipe(fd2) < 0) // 2: pipe2 created
        perror("pipe error");
    else if ((pid = fork()) < 0) // 3: child forked
        perror("fork error");

    else if (pid == 0) {
        pid = fork();   // create grandchild
        if (pid == 0) {
            pid = fork();   // create great-grandchild
            if (pid == 0) {     /* great-grandchild process */
                close(fd1[RD]);
                close(fd1[WR]);
                close(fd2[WR]);
                dup2(fd2[RD], RD);
                execlp("wc", "wc", "-l", NULL);
            } else {    /* grandchild process */
                close(fd1[WR]);
                dup2(fd1[RD], RD);
                close(fd2[RD]);
                dup2(fd2[WR], WR);
                execlp("grep", "grep", argv[1], NULL);
            }
        } else {    /* child process */
            dup2(fd1[WR], WR);
            close(fd1[RD]);
            execlp("ps", "ps", "-A", NULL);
        }
    } else {  /* parent process */
        wait(NULL);
    }
    return 0;
}