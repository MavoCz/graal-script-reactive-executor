(async function test() {
    return await timeout.blockSleep(100, [
        {
            "id": 1,
            "name": "Steve Jobs"
        },
        {
            "id": 2,
            "name": "Bob Balmer"
        }
    ]);
})

