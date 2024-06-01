package util;

/**
 * 🆕提供一个「复制」方法
 * * 🎯约束其实现者「必然实现clone方法」
 * * ⚠️Java中没有在接口中引用「实现者类型」的方法，没有「Self」类型
 * * ❌【2024-06-01 16:04:39】不能用Clone<Self>，会导致泛型传染
 * * 🚩【2024-06-01 16:17:30】目前没用：不同类型需要不同的返回值（类型）
 */
public interface Clone/* <Self> */ extends Cloneable {
    public Object cloneSentence();
}
