export const fakeDelay = async (delayInMillis: number): Promise<void> => {
    await new Promise(resolve => setTimeout(resolve, delayInMillis));
}