import jpype

_org = jpype.JPackage('org')

_Arrays = jpype.java.util.Arrays
_Container = _org.matsim.core.api.internal.MatsimTopLevelContainer
_Id = _org.matsim.api.core.v01.Id


class MatsimContainerCustomizer(jpype.JClassCutomizer):
    def canCustomize(self, name, jc):
        return jc.isIn

    def customize(self, name, jc, bases, members=None, fields=None, **kwargs):
        interfaces = jc.getInterfaces()
        if interfaces.contains(_Container):
            for getter_name, getter in _getStringAccessors(jc):
                if getter_name in members:
                    raise Exception('already a method {} in {}'.format(getter_name, name))
                members[getter_name] = getter


def _getStringAccessors(container_class):
    for method in container_class.getMethods():
        if (method.getName().startsWith("get") and
                method.getName().endsWith("get") and
                method.getParameterTypes().length == 0 and
                method.getGenericReturnType().getTypeName().startsWith("Map<Id")):
            yield _createStringAccessor(method)

def _createStringAccessor(javaMapGetter):
    initial_name = javaMapGetter.getName()
    # remove final "s"
    new_name = initial_name[:-1]

    # Assume the map has typed ids. If not, cannot generate getter anyway...
    map_type = javaMapGetter.getGenericReturnType()
    id_type = map_type.getActualTypeArguments()[0]
    id_type_parameter = id_type.getActualTypeArguments()[0].getRawType()

    def python_accessor(self, id_string):
        id = _Id.create(id_string, id_type_parameter)
        return javaMapGetter.invoke(self).get(id)

    return new_name, python_accessor

jpype.registerClassCustomizer(MatsimContainerCustomizer())