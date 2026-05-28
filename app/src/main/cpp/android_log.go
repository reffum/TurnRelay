package main

//
// Function to write message to android log
//

/*
   #include <stdlib.h>
   #include "turnrelay.h"
*/
import "C"
import "fmt"
import "unsafe"

func LogDebug(format string, args ...any) {
	msg := fmt.Sprintf(format, args)
	msg_c := C.CString(msg)

	defer C.free(unsafe.Pointer(msg_c))

	C.write_log_debug(msg_c)
}

func LogInfo(format string, args ...any) {
	msg := fmt.Sprintf(format, args)
	msg_c := C.CString(msg)

	defer C.free(unsafe.Pointer(msg_c))

	C.write_log_info(msg_c)
}

func LogWarn(format string, args ...any) {
	msg := fmt.Sprintf(format, args)
	msg_c := C.CString(msg)

	defer C.free(unsafe.Pointer(msg_c))

	C.write_log_warning(msg_c)
}

func LogError(format string, args ...any) {
	msg := fmt.Sprintf(format, args)
	msg_c := C.CString(msg)

	defer C.free(unsafe.Pointer(msg_c))

	C.write_log_error(msg_c)
}
