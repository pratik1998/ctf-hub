import ast
import astor

# Load the AST from the file
with open('license_checker.ast', 'rb') as f:
  ast_obj = ast.literal_eval(f.read())

pythopia = astor.to_source(ast_obj)
# print(pythopia)

# # Convert the AST back to Python code
# code = compile(tree, '<ast>', 'exec')

# # Evaluate the Python code
# exec(code)
