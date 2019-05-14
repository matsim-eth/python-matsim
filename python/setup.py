from setuptools import setup, find_packages, Command
from distutils.command.build import build

from buildutils.codegeneration import JavaAdapterCodeGenerationCommand


class MyBuild(build):
    sub_commands = [('codegen', None)] + build.sub_commands

setup(
    name='pythonmatsim',
    version='0.1a1',
    package_dir={'': 'generatedcode/'},
    # Note that this works only when code was already generated... Find a fix.
    packages=find_packages('generatedcode/', exclude=('buildutils', 'test')),
    include_package_data=True,
    url='',
    license='GNU GPL 3.0',
    author='Thibaut Dubernet',
    author_email='thibaut.dubernet@ivt.baug.ethz.ch',
    description='A package to use MATSim from Python',
    setup_requires=[
        'JPype1==0.6.3',
    ],
    install_requires=[
        'JPype1==0.6.3',
    ],
    # Need type hints
    python_requires='>=3.5',
    cmdclass={
      'codegen': JavaAdapterCodeGenerationCommand,
      'build': MyBuild
    },
)
