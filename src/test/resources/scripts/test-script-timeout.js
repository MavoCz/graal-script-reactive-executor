(async function test() {
    return await timeout.ms(100, [
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

