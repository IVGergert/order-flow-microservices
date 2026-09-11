export function showAlert(message, isError = true) {
    const alertBox = document.getElementById("alertBox");

    if (!alertBox) {
        return;
    }

    alertBox.innerText = message;

    alertBox.className = `alert ${
        isError
            ? "alert-danger"
            : "alert-success"
    }`;
}


export function hideAlert() {
    const alertBox = document.getElementById("alertBox");

    if (!alertBox) {
        return;
    }

    alertBox.className = "alert hidden";
}