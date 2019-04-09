public interface Channel<E>
{
    public void send(E item);
    public E receive();
}
