package com.caucho.quercus.lib.spl;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.L10N;

import java.util.Iterator;

public abstract class AbstractIteratorImpl<T> implements Iterator<T> {
    private static final L10N L = new L10N(AbstractIteratorImpl.class);

    protected final Env _env;
    protected final ObjectValue _obj;
    protected final QuercusClass _qClass;

    private final AbstractFunction _nextFun;
    private final AbstractFunction _currentFun;
    private final AbstractFunction _keyFun;
    private final AbstractFunction _rewindFun;
    private final AbstractFunction _validFun;
    private boolean _needNext;

    public AbstractIteratorImpl(Env env, ObjectValue obj)
    {
      _env = env;
      _qClass = obj.getQuercusClass();
      _obj = obj;

      if (!obj.isA("iterator"))
        throw new IllegalStateException(L.l("'{0}' is an invalid iterator",
          obj));
      _currentFun = _qClass.getFunction(env.createString("current"));
      _keyFun = _qClass.getFunction(env.createString("key"));
      _nextFun = _qClass.getFunction(env.createString("next"));
      _rewindFun = _qClass.getFunction(env.createString("rewind"));
      _validFun = _qClass.getFunction(env.createString("valid"));

      _rewindFun.callMethod(_env, _qClass, _obj);

      _needNext = false;
    }

    public boolean hasNext()
    {
      if (_needNext)
        _nextFun.callMethod(_env, _qClass, _obj);

      _needNext = true;

      return _validFun.callMethod(_env, _qClass, _obj).toBoolean();
    }

    public T next()
    {
      return getCurrent();
    }

    abstract protected T getCurrent();

    protected Value getCurrentKey()
    {
      return _keyFun.callMethod(_env, _qClass, _obj);
    }

    protected Value getCurrentValue()
    {
      return _currentFun.callMethod(_env, _qClass, _obj);
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
}