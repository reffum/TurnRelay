//
// Created by oleg on 5/28/26.
//

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
#pragma ide diagnostic ignored "OCUnusedMacroInspection"

#ifndef TURNRELAY_TURNRELAY_H
#define TURNRELAY_TURNRELAY_H

#include <stdbool.h>

#define STATE_DISCONNECT		0
#define STATE_CONNECTED			1
#define STATE_ERROR				2

void TunnelProcess_updateState(int stateId);
bool TunnelProcess_protect(int fd);

void write_log_debug(const char* message);
void write_log_info(const char *message);
void write_log_warning(const char *message);
void write_log_error(const char *message);

#endif //TURNRELAY_TURNRELAY_H

#pragma clang diagnostic pop