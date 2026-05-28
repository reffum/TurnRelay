//
// Created by oleg on 5/28/26.
// Functions exported by main.go
//

#ifndef TURNRELAY_MAIN_GO_EXPORT_H
#define TURNRELAY_MAIN_GO_EXPORT_H

int main_go_start(
        const char* turn_server_address,
        int turn_server_port,
        const char* turn_username,
        const char* turn_password,
        const char* server_address,
        int server_port,
        int tun_fd);

int main_go_stop();



#endif //TURNRELAY_MAIN_GO_EXPORT_H
